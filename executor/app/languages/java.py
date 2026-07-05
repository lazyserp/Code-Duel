IMAGE = "codeduel-runner-java"
FILENAME = "Main.java"


def get_command(filename: str, input_file: str) -> str:
    return f"sh -c 'javac {filename} && java Main < {input_file}'"
