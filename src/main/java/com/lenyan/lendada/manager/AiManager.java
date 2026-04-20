package com.lenyan.lendada.manager;

import ai.z.openapi.service.model.Delta;
import com.github.rholder.retry.*;
import com.lenyan.lendada.common.ErrorCode;
import com.lenyan.lendada.exception.BusinessException;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.*;


import io.reactivex.rxjava3.core.Flowable;
import org.springframework.stereotype.Component;


import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;



/**
 * 通用 AI 调用能力
 */
@Component
public class AiManager {

    @Resource
    private ZhipuAiClient zhipuAiClient;

    // 稳定的随机数
    private static final float STABLE_TEMPERATURE = 0.05f;

    // 不稳定的随机数
    private static final float UNSTABLE_TEMPERATURE = 0.99f;

    /**
     * 同步请求（答案不稳定）
     *
     * @param systemMessage
     * @param userMessage
     * @return
     */
    public String doSyncUnstableRequest(String systemMessage, String userMessage) {
        return doRequest(systemMessage, userMessage, Boolean.FALSE, UNSTABLE_TEMPERATURE);
    }

    /**
     * 同步请求（答案较稳定）
     *
     * @param systemMessage
     * @param userMessage
     * @return
     */
    public String doSyncStableRequest(String systemMessage, String userMessage) {
        return doRequest(systemMessage, userMessage, Boolean.FALSE, STABLE_TEMPERATURE);
    }

    /**
     * 同步请求
     *
     * @param systemMessage
     * @param userMessage
     * @param temperature
     * @return
     */
    public String doSyncRequest(String systemMessage, String userMessage, Float temperature) {
        return doRequest(systemMessage, userMessage, Boolean.FALSE, temperature);
    }

    /**
     * 通用请求（简化消息传递）
     *
     * @param systemMessage
     * @param userMessage
     * @param stream
     * @param temperature
     * @return
     */
    public String doRequest(String systemMessage, String userMessage, Boolean stream, Float temperature) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM.value())
                .content(systemMessage)
                .build());
        messages.add(ChatMessage.builder()
                .role(ChatMessageRole.USER.value())
                .content(userMessage)
                .build());
        return doRequest(messages, stream, temperature);
    }

/*    public String doRequest(String systemMessage, String userMessage, Boolean stream, Float temperature) {
        List<ChatMessage> chatMessageList = new ArrayList<>();
        ChatMessage systemChatMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage);
        chatMessageList.add(systemChatMessage);
        ChatMessage userChatMessage = new ChatMessage(ChatMessageRole.USER.value(), userMessage);
        chatMessageList.add(userChatMessage);
        return doRequest(chatMessageList, stream, temperature);
    }*/

    /**
     * 通用请求
     *
     * @param messages
     * @param stream
     * @param temperature
     * @return
     */
    public String doRequest(List<ChatMessage> messages, Boolean stream, Float temperature) {


        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model("glm-4.7")
                .messages(messages)
                .thinking(ChatThinking.builder().type("enabled").build())
                .temperature(temperature)
                .stream(stream)
                .build();

        try {
            ChatCompletionResponse response =
                    zhipuAiClient.chat().createChatCompletion(request);

            if (!response.isSuccess()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, response.getMsg());
            }

            return response.getData()
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent()
                    .toString();

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }



    /**
     * 通用流式请求（简化消息传递）
     *
     * @param systemMessage
     * @param userMessage
     * @param temperature
     * @return
     */
    public io.reactivex.rxjava3.core.Flowable<Delta> doStreamRequest(String systemMessage, String userMessage, Float temperature) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM.value())
                .content(systemMessage)
                .build());
        messages.add(ChatMessage.builder()
                .role(ChatMessageRole.USER.value())
                .content(userMessage)
                .build());
        return doStreamRequest(messages, temperature);
    }

    /**
     * 通用流式请求
     *
     * @param messages
     * @param temperature
     * @return
     */
    public Flowable<Delta> doStreamRequest(List<ChatMessage> messages, Float temperature) {
        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model("glm-4.7")
                .messages(messages)
                .temperature(temperature)
                .stream(true)
                .build();

//        System.out.println(zhipuAiClient.);

        try {
            ChatCompletionResponse response =
                    zhipuAiClient.chat().createChatCompletion(request);

            if (!response.isSuccess()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, response.getMsg());
            }

            return response.getFlowable()
                    .map(data -> data.getChoices().get(0).getDelta());

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }

}
