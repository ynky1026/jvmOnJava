package com.wangzhen.jvm.instruction.math.neg;

import com.wangzhen.jvm.instruction.base.NoOperandsInstruction;
import com.wangzhen.jvm.runtimeData.JFrame;
import com.wangzhen.jvm.runtimeData.OperandStack;

/*
negative 否定的
 */
public class DNEG extends NoOperandsInstruction {
    @Override
    public void execute(JFrame frame) {
        OperandStack stack = frame.getOperandStack();
        double num = stack.popDouble();
        stack.pushDouble(-num);
    }
}
