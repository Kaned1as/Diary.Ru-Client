package adonai.diary_browser;

public interface IRequestHandler
{
    public void handleBackground(int opCode, Object body);
    public void handleUi(int opCode, Object body);
}
