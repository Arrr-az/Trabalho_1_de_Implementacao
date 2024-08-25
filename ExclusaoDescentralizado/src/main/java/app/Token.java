package app;

public class Token {
    private boolean token = false;

    public Token(boolean token) {
        this.token = token;
    }

    public boolean possuiToken() {
        return token;
    }

    public void setToken(boolean token) {
        this.token = token;
    }
}