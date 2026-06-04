package com.buenws.buenws_backend.API.Configuration.Interceptor;

import com.buenws.buenws_backend.Util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CustomRequestInterceptor implements HandlerInterceptor {

    Logger logger = LoggerFactory.getLogger(CustomRequestInterceptor.class);
    String START_TIME_ATTR = "requestStartTimeMs";

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        Long durationMs = startTime == null ? -1 : System.currentTimeMillis() - startTime;

        logger.info(
                "method={} path={} status={} durationMs={}, ipAddr={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                durationMs,
                RequestUtil.getClientIp(request)
        );

    }
}
