package com.gepardec.llm.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
@SystemMessage(fromResource = "rules-for-entries.txt")
public interface PromptService {

    @UserMessage(""" 
            Here are the time entries: {entries}
            """)
    @SystemMessage("{systemPrompt}")
    String prompt(String entries, String systemPrompt);


}
