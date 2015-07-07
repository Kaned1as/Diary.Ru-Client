package adonai.diary_browser.misc;

import android.os.Handler;

import java.util.ArrayList;

/**
 * Created by adonai on 08.07.15.
 */
public class StepSequence extends ArrayList<Step> {
    
    private int currentStep = 0;

    public void executeNextStep(Handler toExecuteOn) {
        Step current = get(currentStep);
        toExecuteOn.sendMessage(toExecuteOn.obtainMessage(current.getWhat(), current.getArg()));
        ++currentStep;
    }
    
    public void alterNextStep(Object arg) {
        int nextStepIdx = currentStep + 1; 
        if(nextStepIdx >= size())
            throw new IllegalStateException("No next step available!");
        
        Step next = get(currentStep);
        next.setArg(arg);
    }
    
    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }
}
