package com.commonagent.agent;

import com.commonagent.agent.state.AgentState;

/**
 * 工作流节点接口 - 每个节点实现此接口
 */
public interface WorkflowNode {

    /**
     * 执行节点逻辑
     * @param state 当前 Agent 状态
     * @return 更新后的 Agent 状态
     */
    AgentState execute(AgentState state);

    /**
     * 节点名称
     */
    String getNodeName();
}
