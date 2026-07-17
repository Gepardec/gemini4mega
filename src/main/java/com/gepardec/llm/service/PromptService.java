package com.gepardec.llm.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface PromptService {

    @UserMessage("""
            Here are the time entries: {entries}

            CRITICAL: You are a VALIDATOR, not a data processor.

            BAD RESPONSE (DO NOT DO THIS):
            {"activities":[...]} ← WRONG! Don't echo input!

            GOOD RESPONSE (DO THIS):
            {"valid":false,"errors":[...]} ← CORRECT!

            Your response MUST be in this EXACT format:
            {
              "valid": boolean,
              "errors": [
                {
                  "entryIndex": number,
                  "ruleId": "string",
                  "category": "string",
                  "severity": "likely" | "possible",
                  "message": "string in German"
                }
              ]
            }

            RULES:
            - Start with {"valid":
            - If no errors: {"valid":true,"errors":[]}
            - Never return input data
            """)
    @SystemMessage("{systemPrompt}")
    String prompt(String entries, String systemPrompt);


}
