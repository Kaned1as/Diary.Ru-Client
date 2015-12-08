package adonai.diary_browser.entities;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Класс-отражение БД-таблицы credentials на объектную модель.
 * 
 * @author Adonai
 */
@DatabaseTable(tableName = "credentials")
public class CredentialsItem {
    
    @DatabaseField(generatedId = true)
    private Long id;
    
    @DatabaseField(uniqueIndex = true, canBeNull = false)
    private String username;
    
    @DatabaseField(canBeNull = false)
    private String password;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
}
