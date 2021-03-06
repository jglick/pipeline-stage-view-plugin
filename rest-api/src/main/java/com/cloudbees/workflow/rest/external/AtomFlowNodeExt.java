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

import com.cloudbees.workflow.rest.endpoints.flownode.Log;
import com.cloudbees.workflow.rest.hal.Link;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class AtomFlowNodeExt extends FlowNodeExt {

    private List<String> parentNodes = new ArrayList<String>();

    public List<String> getParentNodes() {
        return parentNodes;
    }

    public void setParentNodes(List<String> parentNodes) {
        this.parentNodes = parentNodes;
    }


    public static AtomFlowNodeExt create(FlowNode node) {
        AtomFlowNodeExt flowNodeExt = new AtomFlowNodeExt();
        flowNodeExt.addBasicNodeData(node);
        if (flowNodeExt.getStatus() != StatusExt.NOT_EXECUTED) {
            flowNodeExt.get_links().setLog(Link.newLink(Log.getUrl(node)));
        }
        flowNodeExt.addParentNodeRefs(node);
        return flowNodeExt;
    }

    private void addParentNodeRefs(FlowNode node) {
        List<FlowNode> parents = node.getParents();
        if (parents != null && !parents.isEmpty()) {
            for (FlowNode parent : parents) {
                getParentNodes().add(parent.getId());
            }
        }
    }

}
