-- V8__update_starter_code_to_leetcode_style.sql

--  Update String Problems (15) to LeetCode-style Solution class
UPDATE problems
SET starter_code = '{"java":"import java.util.*;\n\nclass Solution {\n    public Object solve(String s) {\n        // write your logic here\n        return \"\";\n    }\n}","python":"class Solution:\n    def solve(self, s: str):\n        # write your logic here\n        return \"\"\n","cpp":"#include <iostream>\n#include <string>\n#include <vector>\n#include <algorithm>\nusing namespace std;\n\nclass Solution {\npublic:\n    string solve(string s) {\n        // write your logic here\n        return \"\";\n    }\n};"}'::jsonb
WHERE starter_code::text LIKE '%sc.nextLine().trim()%';

--  Update Array Problems (15) to LeetCode-style Solution class
UPDATE problems
SET starter_code = '{"java":"import java.util.*;\n\nclass Solution {\n    public Object solve(int[] nums) {\n        // write your logic here\n        return 0;\n    }\n}","python":"class Solution:\n    def solve(self, nums: list[int]):\n        # write your logic here\n        return 0\n","cpp":"#include <iostream>\n#include <vector>\n#include <string>\n#include <algorithm>\nusing namespace std;\n\nclass Solution {\npublic:\n    int solve(vector<int>& nums) {\n        // write your logic here\n        return 0;\n    }\n};"}'::jsonb
WHERE starter_code::text LIKE '%sc.nextLine().replaceAll%';
