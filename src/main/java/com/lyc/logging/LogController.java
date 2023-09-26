package com.lyc.logging;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogController {
    @Resource
    private LogMapper logMapper;
    public void insert(LogType logType){
        int count = logMapper.insert(logType);
    }
}
