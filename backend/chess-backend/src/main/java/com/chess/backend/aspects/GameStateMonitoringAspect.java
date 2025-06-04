package com.chess.backend.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
public class GameStateMonitoringAspect {
    private static final Logger logger = LoggerFactory.getLogger(GameStateMonitoringAspect.class);

    @Around("execution(* com.chess.backend.services.GameService.makeMove(..)) || " +
            "execution(* com.chess.backend.services.GameService.getGame(..)) || " +
            "execution(* com.chess.backend.services.GameService.createGame(..))")
    public Object monitorGameStateOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();
            
            logger.info("Game state operation '{}' completed in {} ms",
                joinPoint.getSignature().getName(),
                stopWatch.getTotalTimeMillis());
            
            return result;
        } catch (Exception e) {
            stopWatch.stop();
            logger.error("Game state operation '{}' failed after {} ms: {}",
                joinPoint.getSignature().getName(),
                stopWatch.getTotalTimeMillis(),
                e.getMessage());
            throw e;
        }
    }
} 