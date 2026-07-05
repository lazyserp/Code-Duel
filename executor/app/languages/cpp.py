IMAGE = "codeduel-runner-cpp"
FILENAME = "solution.cpp"


def get_command(filename: str, input_file: str) -> str:
    return f"sh -c 'g++ -o solution {filename} -std=c++17 && ./solution < {input_file}'"
