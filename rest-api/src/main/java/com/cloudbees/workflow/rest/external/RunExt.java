/*
 * The MIT License
 *
 * Copyright (c) 2013-2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.workflow.rest.external;

import com.cloudbees.workflow.rest.endpoints.RunAPI;
import com.cloudbees.workflow.rest.hal.Link;
import com.cloudbees.workflow.rest.hal.Links;
import com.fasterxml.jackson.annotation.JsonInclude;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.input.InputAction;
import org.jenkinsci.plugins.workflow.support.steps.input.InputStepExecution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class RunExt {

    private static final int MAX_ARTIFACTS_COUNT = 100;

    private RunLinks _links;
    private String id;
    private String name;
    private StatusExt status;
    private long startTimeMillis;
    private long endTimeMillis;
    private long durationMillis;
    private long queueDurationMillis;
    private long pauseDurationMillis;
    private List<StageNodeExt> stages;

    public RunLinks get_links() {
        return _links;
    }

    public void set_links(RunLinks _links) {
        this._links = _links;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StatusExt getStatus() {
        return status;
    }

    public void setStatus(StatusExt status) {
        this.status = status;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    public void setEndTimeMillis(long endTimeMillis) {
        this.endTimeMillis = endTimeMillis;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public long getQueueDurationMillis() {
        return queueDurationMillis;
    }

    public void setQueueDurationMillis(long queueDurationMillis) {
        this.queueDurationMillis = queueDurationMillis;
    }

    public long getPauseDurationMillis() {
        return pauseDurationMillis;
    }

    public void setPauseDurationMillis(long pauseDurationMillis) {
        this.pauseDurationMillis = pauseDurationMillis;
    }

    public List<StageNodeExt> getStages() {
        return stages;
    }

    public void setStages(List<StageNodeExt> stages) {
        this.stages = stages;
    }

    public static final class RunLinks extends Links {
        private Link changesets;
        private Link pendingInputActions;
        private Link nextPendingInputAction;
        private Link artifacts;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Link getChangesets() {
            return changesets;
        }

        public void setChangesets(Link changesets) {
            this.changesets = changesets;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Link getPendingInputActions() {
            return pendingInputActions;
        }

        public void setPendingInputActions(Link pendingInputActions) {
            this.pendingInputActions = pendingInputActions;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Link getNextPendingInputAction() {
            return nextPendingInputAction;
        }

        public void setNextPendingInputAction(Link nextPendingInputAction) {
            this.nextPendingInputAction = nextPendingInputAction;
        }

        public Link getArtifacts() {
            return artifacts;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public void setArtifacts(Link artifacts) {
            this.artifacts = artifacts;
        }
    }

    public static RunExt create(WorkflowRun run) {
        final RunExt runExt = new RunExt();

        FlowExecution execution = run.getExecution();

        runExt.set_links(new RunLinks());
        runExt.get_links().initSelf(RunAPI.getDescribeUrl(run));

        runExt.setId(run.getId());
        runExt.setName(run.getDisplayName());
        runExt.initStatus(run);
        runExt.setStartTimeMillis(run.getStartTimeInMillis());
        runExt.setStages(new ArrayList<StageNodeExt>());

        if (execution != null) {
            if (ChangeSetExt.hasChanges(run)) {
                runExt.get_links().setChangesets(Link.newLink(RunAPI.getChangeSetsUrl(run)));
            }
            if (isPendingInput(run)) {
                runExt.get_links().setPendingInputActions(Link.newLink(RunAPI.getPendingInputActionsUrl(run)));
                runExt.get_links().setNextPendingInputAction(Link.newLink(RunAPI.getNextPendingInputActionUrl(run)));
            }
            List<Run<WorkflowJob, WorkflowRun>.Artifact> artifacts = run.getArtifactsUpTo(MAX_ARTIFACTS_COUNT);
            if (artifacts != null && !artifacts.isEmpty()) {
                runExt.get_links().setArtifacts(Link.newLink(RunAPI.getArtifactsUrl(run)));
            }

            FlowGraphWalker walker = new FlowGraphWalker(run.getExecution());
            for (FlowNode node : walker) {
                long nodeTime = TimingAction.getStartTime(node);

                if (nodeTime > runExt.getEndTimeMillis()) {
                    // Use the most resent FlowNode timestamp as being
                    // the end time for the run.
                    runExt.setEndTimeMillis(nodeTime);
                }

                if (StageNodeExt.isStageNode(node)) {
                    StageNodeExt stage = StageNodeExt.create(node);
                    runExt.addStage(stage);
                    runExt.setPauseDurationMillis(runExt.getPauseDurationMillis() + stage.getPauseDurationMillis());
                }
            }

            if (!runExt.getStages().isEmpty()) {
                runExt.sortStages();

                FlowNodeExt lastStage = runExt.getLastStage();
                lastStage.setStatus(runExt.getStatus());
            }

            long currentTimeMillis = System.currentTimeMillis();

            if (runExt.getStatus() == StatusExt.IN_PROGRESS || runExt.getStatus() == StatusExt.PAUSED_PENDING_INPUT) {
                runExt.setEndTimeMillis(currentTimeMillis);
            }

            if (runExt.getStages().isEmpty()) {
                runExt.setQueueDurationMillis(currentTimeMillis - runExt.getStartTimeMillis());
            } else {
                StageNodeExt firstExecutedStage = runExt.getFirstExecutedStage();
                if (firstExecutedStage != null) {
                    runExt.setQueueDurationMillis(firstExecutedStage.getStartTimeMillis() - runExt.getStartTimeMillis());
                }
            }

            runExt.setDurationMillis(Math.max(0, runExt.getEndTimeMillis() - runExt.getStartTimeMillis() - runExt.getQueueDurationMillis()));
        }

        return runExt;
    }

    public static boolean isPendingInput(WorkflowRun run) {
        InputAction inputAction = run.getAction(InputAction.class);
        if (inputAction != null) {
            List<InputStepExecution> executions = inputAction.getExecutions();
            if (executions != null && !executions.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void initStatus(WorkflowRun run) {
        FlowExecution execution = run.getExecution();

        if (execution == null) {
            setStatus(StatusExt.NOT_EXECUTED);
        } else if (execution.getCauseOfFailure() != null) {
            setStatus(StatusExt.valueOf(execution.getCauseOfFailure()));
        } else if (execution.isComplete()) {
            setStatus(StatusExt.SUCCESS);
        } else if (isPendingInput(run)) {
            setStatus(StatusExt.PAUSED_PENDING_INPUT);
        } else {
            setStatus(StatusExt.IN_PROGRESS);
        }
    }

    private void addStage(StageNodeExt stageToAdd) {
        // Iterate the current list of stages and see if we already have a stage with
        // the same id.
        for (int i = 0; i < getStages().size(); i++) {
            FlowNodeExt stage = getStages().get(i);
            if (stage.getId().equals(stageToAdd.getId())) {
                // Same stage... don't add again.
                return;
            }
        }

        // Insert at the head, but we'll be sorting them (by time) later.
        getStages().add(0, stageToAdd);
    }

    private void sortStages() {
        // TF: Sorting in case they're somehow out of order (depending on how the FloWGraph works
        // wrt forks and joins etc).  My understanding is that forks and joins only make sense
        // within a given stage (and so a stage node should only ever be traversed once),
        // but I may be wrong so just in case :) ...
        Comparator<FlowNodeExt> sortComparator = new Comparator<FlowNodeExt>() {
            @Override
            public int compare(FlowNodeExt stage1, FlowNodeExt stage2) {
                if (stage1.getStartTimeMillis() < stage2.getStartTimeMillis()) {
                    return -1;
                } else if (stage1.getStartTimeMillis() > stage2.getStartTimeMillis()) {
                    return 1;
                }
                return 0;
            }
        };

        Collections.sort(getStages(), sortComparator);
    }

    private StageNodeExt getFirstExecutedStage() {
        for (int i = 0; i < getStages().size(); i++) {
            StageNodeExt stage = getStages().get(i);
            if (stage.getStatus() != StatusExt.NOT_EXECUTED) {
                return stage;
            }
        }
        return null;
    }

    private FlowNodeExt getLastStage() {
        if (getStages().isEmpty()) {
            return null;
        } else {
            return getStages().get(getStages().size() - 1);
        }
    }
}
