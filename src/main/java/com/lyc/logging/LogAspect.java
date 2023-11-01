package com.lyc.logging;


import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@Aspect
@Slf4j
public class LogAspect {
    @Autowired
    private LogMapper logMapper;
    @Pointcut("@annotation(com.lyc.logging.LogAnnotation)")
    public void pt(){

    }

    @Around("pt()")
    public  Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            long begintime = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long time = System.currentTimeMillis() - begintime;
            recordLog(joinPoint, time);
            return result;
        }catch (Throwable e){
            logError(joinPoint,e.getMessage());
            throw e;
        }

    }

    private void recordLog(ProceedingJoinPoint joinPoint, long time){
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogAnnotation logAnnotation = method.getAnnotation(LogAnnotation.class);
        log.info("==============log start================");
        log.info("module:{}", logAnnotation.module());
        log.info("operation:{}", logAnnotation.operator());

        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        log.info("request.method:{}", className + "." + methodName + "()");
        Object[] args = joinPoint.getArgs();
        if(args.length == 0){
            log.info("params: no params");
        }
        else{
            String params = JSON.toJSONString(args[0]);
            log.info("params:{}",params);
        }

        log.info("execute time: {}ms", time);
        log.info("==============log end================");

        LogType logType = new LogType();
        logType.setClassName(className);
        logType.setMethodName(methodName);
        logType.setTime(time);
        logType.setStatus("Success!");
        int count = logMapper.insert(logType);

    }

    private void logError(ProceedingJoinPoint joinPoint, String exceptionMessage) throws JsonProcessingException {
        String targetClassName = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        String params = new ObjectMapper().writeValueAsString(joinPoint.getArgs());
        log.error("error.message: {}--->{}--->{}",targetClassName+"."+methodName,params,exceptionMessage);
        String logMessage = String.format("error.message: %s--->%s--->%s", targetClassName + "." + methodName, params, exceptionMessage);
        LogType logType = new LogType();
        logType.setClassName(targetClassName);
        logType.setMethodName(methodName);
        logType.setTime(0);
        logType.setStatus("Fail!");
        logType.setErrorMsg(logMessage);
        int count = logMapper.insert(logType);
    }
}