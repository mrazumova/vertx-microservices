package app;

import java.util.Objects;

public class User {

    private int id;

    private String username;

    private String country;

    private int age;

    public User(int id, String username, String country, int age) {
        this.id = id;
        this.username = username;
        this.country = country;
        this.age = age;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id &&
                age == user.age &&
                username.equals(user.username) &&
                country.equals(user.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, country, age);
    }
}
