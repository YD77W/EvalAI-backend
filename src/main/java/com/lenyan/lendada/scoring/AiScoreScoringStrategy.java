package com.lenyan.lendada.scoring;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lenyan.lendada.manager.AiManager;
import com.lenyan.lendada.model.dto.question.QuestionAnswerDTO;
import com.lenyan.lendada.model.dto.question.QuestionContentDTO;
import com.lenyan.lendada.model.entity.*;
import com.lenyan.lendada.model.vo.QuestionVO;
import com.lenyan.lendada.service.QuestionService;
import com.lenyan.lendada.service.ScoringResultService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ✅ 得分类应用 · 最佳混合评分策略
 * 后端精确算分 + AI 负责评价说明
 * apptype对应 应用测评类型（ 1 = 测评/ 0 = 得分）   scoringStrategy对应评分策略（ 1 = AI/ 0 = 自定义）
 */
@ScoringStrategyConfig(appType = 0, scoringStrategy = 1)
public class AiScoreScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;

    @Resource
    private AiManager aiManager;

    private static final String AI_SCORE_EVAL_SYSTEM_MESSAGE =
            "你是一位严谨的测评分析专家，我会给你如下信息：\n" +
                    "```\n" +
                    "应用名称，\n" +
                    "【【【应用描述】】】，\n" +
                    "用户最终得分（整数），\n" +
                    "题目和用户回答的列表：格式为 [{\"title\": \"题目\",\"answer\": \"用户回答\"}]\n" +
                    "```\n" +
                    "请你根据上述信息，对用户的整体表现进行分析说明：\n" +
                    "1. 给出一个评价名称（简短）\n" +
                    "2. 给出评价描述，必须结合用户得分进行解释（150字左右）\n" +
                    "3. 严格返回 JSON：{\"resultName\":\"\",\"resultDesc\":\"\"}";

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {

        Long appId = app.getId();

        // 1️⃣ 查题目
        Question question = questionService.getOne(
                Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId)
        );
        QuestionVO questionVO = QuestionVO.objToVo(question);
        List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();

        // 2️⃣ 精确算分（与你 CustomScore 完全一致）
        int totalScore = 0;
        for (int i = 0; i < questionContent.size(); i++) {
            QuestionContentDTO q = questionContent.get(i);
            String userAnswerKey = choices.size() > i ? choices.get(i) : null;
            if (userAnswerKey == null) continue;

            for (QuestionContentDTO.Option option : q.getOptions()) {
                if (option.getKey().equals(userAnswerKey)) {
                    totalScore += Optional.of(option.getScore()).orElse(0);
                    break;
                }
            }
        }

        // 3️⃣ 匹配档位
        List<ScoringResult> scoringResultList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, appId)
                        .orderByDesc(ScoringResult::getResultScoreRange)
        );


        ScoringResult finalResult = null;

        if (scoringResultList != null && !scoringResultList.isEmpty()) {
            for (ScoringResult r : scoringResultList) {
                if (totalScore >= r.getResultScoreRange()) {
                    finalResult = r;
                    break;
                }
            }
            // 如果都没命中，兜底用最低档
            if (finalResult == null) {
                finalResult = scoringResultList.get(scoringResultList.size() - 1);
            }
        }


        // 4️⃣ AI 评价
        String aiUserMessage = buildAiUserMessage(app, questionContent, choices, totalScore);
        String aiResult = aiManager.doSyncStableRequest(
                AI_SCORE_EVAL_SYSTEM_MESSAGE, aiUserMessage
        );

        String json = aiResult.substring(aiResult.indexOf("{"), aiResult.lastIndexOf("}") + 1);
        UserAnswer aiEval = JSONUtil.toBean(json, UserAnswer.class);

        // 5️⃣ 组装最终结果
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultScore(totalScore);

//        userAnswer.setResultId(finalResult.getId());
        userAnswer.setResultName(aiEval.getResultName());
        userAnswer.setResultDesc(aiEval.getResultDesc());
//        userAnswer.setResultPicture(finalResult.getResultPicture());

        if (finalResult != null) {
            userAnswer.setResultId(finalResult.getId());
            userAnswer.setResultPicture(finalResult.getResultPicture());
        }

        return userAnswer;
    }

    private String buildAiUserMessage(App app,
                                      List<QuestionContentDTO> questionContent,
                                      List<String> choices,
                                      int totalScore) {

        List<QuestionAnswerDTO> list = new ArrayList<>();
        for (int i = 0; i < questionContent.size(); i++) {
            QuestionAnswerDTO dto = new QuestionAnswerDTO();
            dto.setTitle(questionContent.get(i).getTitle());
            dto.setUserAnswer(choices.get(i));
            list.add(dto);
        }

        return app.getAppName() + "\n"
                + app.getAppDesc() + "\n"
                + "用户最终得分：" + totalScore + "\n"
                + JSONUtil.toJsonStr(list);
    }
}