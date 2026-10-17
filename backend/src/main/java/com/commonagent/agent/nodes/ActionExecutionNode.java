package com.commonagent.agent.nodes;

import com.commonagent.agent.WorkflowNode;
import com.commonagent.agent.state.AgentState;
import com.commonagent.agent.state.AgentState.ActionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 动作执行节点
 * 根据意图执行具体业务动作:
 * - ORDER_QUERY: 查询订单
 * - REFUND: 发起退款
 * - APPOINTMENT: 创建预约
 * 可通过插件机制扩展，不同行业接入不同业务系统
 */
@Slf4j
@Component
public class ActionExecutionNode implements WorkflowNode {

    @Override
    public AgentState execute(AgentState state) {
        log.debug("[ActionExecution] tenant={}, intent={}",
                state.getTenantId(),
                state.getIntentResult() != null ? state.getIntentResult().getIntentType() : "unknown");

        if (state.getIntentResult() == null) {
            state.setCurrentNode(getNodeName());
            return state;
        }

        String intentType = state.getIntentResult().getIntentType();
        ActionResult result = switch (intentType) {
            case "ORDER_QUERY" -> executeOrderQuery(state);
            case "REFUND" -> executeRefund(state);
            case "APPOINTMENT" -> executeAppointment(state);
            default -> ActionResult.builder()
                    .actionType(intentType)
                    .success(true)
                    .resultData("无特定动作需要执行")
                    .build();
        };

        state.setActionResult(result);
        state.setCurrentNode(getNodeName());
        return state;
    }

    private ActionResult executeOrderQuery(AgentState state) {
        // TODO: 对接具体业务系统的订单查询 API
        // 通过租户配置获取对应系统的接口信息
        log.info("[ActionExecution] 查询订单 - 租户: {}, 用户: {}",
                state.getTenantId(), state.getUserId());

        return ActionResult.builder()
                .actionType("ORDER_QUERY")
                .success(true)
                .resultData("订单查询功能待对接业务系统")
                .build();
    }

    private ActionResult executeRefund(AgentState state) {
        // TODO: 对接退款系统
        log.info("[ActionExecution] 发起退款 - 租户: {}", state.getTenantId());

        return ActionResult.builder()
                .actionType("REFUND")
                .success(true)
                .resultData("退款功能待对接业务系统")
                .build();
    }

    private ActionResult executeAppointment(AgentState state) {
        // TODO: 对接预约系统
        log.info("[ActionExecution] 创建预约 - 租户: {}", state.getTenantId());

        return ActionResult.builder()
                .actionType("APPOINTMENT")
                .success(true)
                .resultData("预约功能待对接业务系统")
                .build();
    }

    @Override
    public String getNodeName() {
        return "ACTION_EXECUTION";
    }
}
