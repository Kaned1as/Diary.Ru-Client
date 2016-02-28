package adonai.diary_browser.entities;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.sql.Timestamp;

/**
 * Класс-отражение БД-таблицы credentials на объектную модель.
 * 
 * @author Adonai
 */
@DatabaseTable(tableName = "credentials")
public class CredentialsItem {
    
    @DatabaseField(id = true)
    private String username;
    
    @DatabaseField(canBeNull = false)
    private String password;

    @DatabaseField
    private boolean autologin;

    @DatabaseField
    private int flags;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAutologin() {
        return autologin;
    }

    public void setAutologin(boolean autologin) {
        this.autologin = autologin;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CredentialsItem that = (CredentialsItem) o;

        if (autologin != that.autologin) return false;
        if (flags != that.flags) return false;
        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        return !(password != null ? !password.equals(that.password) : that.password != null);

    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (autologin ? 1 : 0);
        result = 31 * result + flags;
        return result;
    }

    /**
     * Перегружено, чтобы работали адаптеры автоподсказок
     * @return username
     */
    @Override
    public String toString() {
        return username;
    }
}
