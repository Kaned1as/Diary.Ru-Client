package adonai.diary_browser.misc;

/**
 * Реализация последовательности шагов при получении сообщения
 */
public class Step {
    
    private int what;
    private Object arg;

    public Step(int what, Object arg) {
        this.what = what;
        this.arg = arg;
    }

    public int getWhat() {
        return what;
    }

    public void setWhat(int what) {
        this.what = what;
    }

    public Object getArg() {
        return arg;
    }

    public void setArg(Object arg) {
        this.arg = arg;
    }
}
