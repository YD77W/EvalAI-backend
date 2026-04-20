package com.lenyan.lendada.model.dto.statistic;

import lombok.Data;

/**
 * App 用户提交答案书统计
 */
@Data
public class AppAnswerCountDTO {

    private String appId;

    // 新增应用名称
    private String appName;

    /**
     * 用户提交答案数
     */
    private Long answerCount;
}