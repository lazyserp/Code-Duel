package com.codeduel.codeduel.arena.seeder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.codeduel.codeduel.arena.repository.ProblemRepository;
import com.codeduel.codeduel.arena.model.Difficulty;
import com.codeduel.codeduel.arena.model.Problem;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProblemSeeder implements CommandLineRunner {

    private final ProblemRepository problemRepository;

    @Override
    public void run(String... args) throws Exception {
        if (problemRepository.count() == 0) {
            
            Problem p1 = Problem.builder()
                .title("Contains Duplicate")
                .description("Given an integer array `nums`, return `true` if any value appears at least twice in the array, and return `false` if every element is distinct.\n\n**Example 1:**\n```\nInput: nums = [1,2,3,1]\nOutput: true\n```\n**Example 2:**\n```\nInput: nums = [1,2,3,4]\nOutput: false\n```")
                .difficulty(Difficulty.EASY)
                .testCases("[{\"input\":\"[1,2,3,1]\",\"output\":\"true\"},{\"input\":\"[1,2,3,4]\",\"output\":\"false\"},{\"input\":\"[1,1,1,3,3,4,3,2,4,2]\",\"output\":\"true\"}]")
                .starterCode("{\"java\":\"class Solution {\\n    public boolean containsDuplicate(int[] nums) {\\n        \\n    }\\n}\"}")
                .build();

            Problem p2 = Problem.builder()
                .title("Valid Anagram")
                .description("Given two strings `s` and `t`, return `true` if `t` is an anagram of `s`, and `false` otherwise. An anagram is a word formed by rearranging the letters of another word using all original letters exactly once.\n\n**Example 1:**\n```\nInput: s = \"anagram\", t = \"nagaram\"\nOutput: true\n```\n**Example 2:**\n```\nInput: s = \"rat\", t = \"car\"\nOutput: false\n```")
                .difficulty(Difficulty.EASY)
                .testCases("[{\"input\":\"anagram\\nnagaram\",\"output\":\"true\"},{\"input\":\"rat\\ncar\",\"output\":\"false\"}]")
                .starterCode("{\"java\":\"class Solution {\\n    public boolean isAnagram(String s, String t) {\\n        \\n    }\\n}\"}")
                .build();

            Problem p3 = Problem.builder()
                .title("Two Sum")
                .description("Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`. You may assume that each input would have exactly one solution, and you may not use the same element twice.\n\n**Example 1:**\n```\nInput: nums = [2,7,11,15], target = 9\nOutput: [0,1]\nExplanation: nums[0] + nums[1] == 9, so we return [0, 1].\n```")
                .difficulty(Difficulty.EASY)
                .testCases("[{\"input\":\"[2,7,11,15]\\n9\",\"output\":\"[0,1]\"},{\"input\":\"[3,2,4]\\n6\",\"output\":\"[1,2]\"},{\"input\":\"[3,3]\\n6\",\"output\":\"[0,1]\"}]")
                .starterCode("{\"java\":\"class Solution {\\n    public int[] twoSum(int[] nums, int target) {\\n        \\n    }\\n}\"}")
                .build();

            Problem p4 = Problem.builder()
                .title("Best Time to Buy and Sell Stock")
                .description("You are given an array `prices` where `prices[i]` is the price of a given stock on the ith day. You want to maximize your profit by choosing a single day to buy one stock and choosing a different day in the future to sell that stock. Return the maximum profit you can achieve from this transaction. If you cannot achieve any profit, return 0.\n\n**Example 1:**\n```\nInput: prices = [7,1,5,3,6,4]\nOutput: 5\nExplanation: Buy on day 2 (price = 1) and sell on day 5 (price = 6), profit = 6-1 = 5.\n```")
                .difficulty(Difficulty.EASY)
                .testCases("[{\"input\":\"[7,1,5,3,6,4]\",\"output\":\"5\"},{\"input\":\"[7,6,4,3,1]\",\"output\":\"0\"}]")
                .starterCode("{\"java\":\"class Solution {\\n    public int maxProfit(int[] prices) {\\n        \\n    }\\n}\"}")
                .build();

            Problem p5 = Problem.builder()
                .title("Valid Parentheses")
                .description("Given a string `s` containing just the characters `'('`, `')'`, `'{'`, `'}'`, `'['` and `']'`, determine if the input string is valid. An input string is valid if: Open brackets must be closed by the same type of brackets, and open brackets must be closed in the correct order.\n\n**Example 1:**\n```\nInput: s = \"()\"\nOutput: true\n```\n**Example 2:**\n```\nInput: s = \"()[]{}\"\nOutput: true\n```\n**Example 3:**\n```\nInput: s = \"(]\"\nOutput: false\n```")
                .difficulty(Difficulty.EASY)
                .testCases("[{\"input\":\"()\",\"output\":\"true\"},{\"input\":\"()[]{}\",\"output\":\"true\"},{\"input\":\"(]\",\"output\":\"false\"}]")
                .starterCode("{\"java\":\"class Solution {\\n    public boolean isValid(String s) {\\n        \\n    }\\n}\"}")
                .build();

            problemRepository.saveAll(java.util.List.of(p1, p2, p3, p4, p5));
        }
    }
}
