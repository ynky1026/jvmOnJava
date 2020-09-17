package com.wangzhen.algorithm.leetcode.lt_32;

import com.wangzhen.algorithm.leetcode.common.TreeNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Description: 剑指 Offer 32 - I. 从上到下打印二叉树
 *                 Breadth First Search BFS 广度优先搜索
 * Datetime:    2020/9/17   2:29 下午
 * Author:   王震
 */
class Solution {
    public int[] levelOrder(TreeNode root) {
        if(root == null){
            return new int[0];
        }
        LinkedList<TreeNode> list = new LinkedList<>();
        ArrayList<Integer> ans = new ArrayList<>();
        list.add(root);
        while (!list.isEmpty()){
            TreeNode treeNode = list.poll();
            //System.out.println(treeNode.val);
            ans.add(treeNode.val);
            if(treeNode.left!=null){
                list.add(treeNode.left);
            }
            if(treeNode.right!=null){
                list.add(treeNode.right);
            }

        }
        int [] levelOrders = new int[ans.size()];
        for(int i=0;i<ans.size();i++){
            levelOrders[i] = ans.get(i);
        }

        return levelOrders;
    }
}
