IMAGE = "codeduel-runner-python"
FILENAME = "solution.py"


def get_command(filename: str, input_file: str) -> str:
    return f"sh -c 'python3 {filename} < {input_file}'"
