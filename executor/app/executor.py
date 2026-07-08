import base64
import io
import os
import tarfile
import time
import docker

from app.models import Submission, SubmissionResult
from app import languages

_client = docker.from_env()

_LANG_MAP = {
    "java": languages.java,
    "python": languages.python,
    "cpp": languages.cpp,
}


def _wrap_source(source: str, language: str, is_array: bool) -> str:
    if language == "java":
        if is_array:
            driver = """import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextLine()) return;
        String line = sc.nextLine().replaceAll("[\\\\\\\\[\\\\\\\\]]", "");
        int[] nums = line.isEmpty() ? new int[0] : Arrays.stream(line.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
        Solution solution = new Solution();
        Object res = solution.solve(nums);
        System.out.println(res);
    }
}
"""
        else:
            driver = """import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.hasNextLine() ? sc.nextLine() : "";
        Solution solution = new Solution();
        Object res = solution.solve(s);
        System.out.println(res);
    }
}
"""
        return driver + "\n" + source

    elif language == "python":
        if is_array:
            driver = """
import sys
if __name__ == "__main__":
    line = sys.stdin.read().strip()
    line = line.replace('[', '').replace(']', '')
    nums = [int(x.strip()) for x in line.split(',') if x.strip()] if line else []
    sol = Solution()
    res = sol.solve(nums)
    if isinstance(res, bool):
        print(str(res).lower())
    else:
        print(res)
"""
        else:
            driver = """
import sys
if __name__ == "__main__":
    s = sys.stdin.read().rstrip('\\r\\n')
    sol = Solution()
    res = sol.solve(s)
    if isinstance(res, bool):
        print(str(res).lower())
    else:
        print(res)
"""
        return source + "\n" + driver

    elif language == "cpp":
        if is_array:
            driver = """
#include <iostream>
#include <vector>
#include <string>
#include <sstream>
#include <algorithm>
using namespace std;

int main() {
    string line;
    if (getline(cin, line)) {
        line.erase(remove(line.begin(), line.end(), '['), line.end());
        line.erase(remove(line.begin(), line.end(), ']'), line.end());
        stringstream ss(line);
        vector<int> nums;
        string token;
        while (getline(ss, token, ',')) {
            if (!token.empty()) {
                nums.push_back(stoi(token));
            }
        }
        Solution solution;
        auto res = solution.solve(nums);
        cout << boolalpha << res << endl;
    }
    return 0;
}
"""
        else:
            driver = """
#include <iostream>
#include <string>
using namespace std;

int main() {
    string s;
    if (getline(cin, s)) {
        Solution solution;
        auto res = solution.solve(s);
        cout << boolalpha << res << endl;
    }
    return 0;
}
"""
        return source + "\n" + driver

    return source


def _b64_decode(value: str) -> str:
    if not value:
        return ""
    return base64.b64decode(value).decode("utf-8")


def _b64_encode(value: str) -> str | None:
    if not value:
        return None
    return base64.b64encode(value.encode("utf-8")).decode("utf-8")


def _make_tar(files: dict[str, str]) -> bytes:
    """Pack {filename: content} into an in-memory tar archive."""
    buf = io.BytesIO()
    with tarfile.open(fileobj=buf, mode="w") as tar:
        for name, content in files.items():
            data = content.encode("utf-8")
            info = tarfile.TarInfo(name=name)
            info.size = len(data)
            info.mode = 0o644
            tar.addfile(info, io.BytesIO(data))
    return buf.getvalue()


def execute(submission: Submission) -> SubmissionResult:
    lang = _LANG_MAP.get(submission.language)
    if lang is None:
        return SubmissionResult(
            status="RUNTIME_ERROR",
            time=0.0,
            stderr=_b64_encode(f"Unsupported language: {submission.language}"),
        )

    source = _b64_decode(submission.source_code)
    stdin = _b64_decode(submission.stdin)
    
    is_array = stdin.strip().startswith('[') and stdin.strip().endswith(']')
    source = _wrap_source(source, submission.language, is_array)
    
    expected = _b64_decode(submission.expected_output).strip()
    mem_limit = f"{submission.memory_limit}m"
    command = lang.get_command(lang.FILENAME, "input.txt")


    container = None
    try:
        container = _client.containers.create(
            image=lang.IMAGE,
            command=command,
            mem_limit=mem_limit,
            nano_cpus=500_000_000,
            network_mode="none",
            pids_limit=64,
            working_dir="/code",
            user="runner",
        )

        # Copy source + input directly into the container — no volume mount needed
        tar_data = _make_tar({lang.FILENAME: source, "input.txt": stdin})
        container.put_archive("/code", tar_data)

        start = time.monotonic()
        container.start()

        try:
            result = container.wait(timeout=submission.time_limit)
            elapsed = time.monotonic() - start
            exit_code = result["StatusCode"]
        except Exception:
            elapsed = time.monotonic() - start
            try:
                container.kill()
            except Exception:
                pass
            return SubmissionResult(status="TLE", time=round(elapsed, 3))

        stdout_raw = container.logs(stdout=True, stderr=False).decode("utf-8", errors="replace")
        stderr_raw = container.logs(stdout=False, stderr=True).decode("utf-8", errors="replace")

        if exit_code != 0:
            # Detect compile error by checking if output artifact was produced
            compile_failed = False
            if submission.language == "java":
                try:
                    bits, _ = container.get_archive("/code/Main.class")
                    compile_failed = False
                except Exception:
                    compile_failed = True
            elif submission.language == "cpp":
                try:
                    bits, _ = container.get_archive("/code/solution")
                    compile_failed = False
                except Exception:
                    compile_failed = True

            if compile_failed:
                return SubmissionResult(
                    status="COMPILATION_ERROR",
                    time=round(elapsed, 3),
                    compile_output=_b64_encode(stderr_raw),
                )
            return SubmissionResult(
                status="RUNTIME_ERROR",
                time=round(elapsed, 3),
                stderr=_b64_encode(stderr_raw),
            )

        actual = stdout_raw.strip()
        status = "ACCEPTED" if actual == expected else "WRONG_ANSWER"
        return SubmissionResult(
            status=status,
            time=round(elapsed, 3),
            stderr=_b64_encode(stderr_raw),
        )

    except docker.errors.ImageNotFound:
        raise
    except Exception as e:
        return SubmissionResult(status="RUNTIME_ERROR", time=0.0, stderr=_b64_encode(str(e)))
    finally:
        if container:
            try:
                container.remove(force=True)
            except Exception:
                pass
