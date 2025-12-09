package common;
import java.util.Objects;

public final class ObjectId {
    private final String value;

    public ObjectId(String value) {
        this.value = Objects.requireNonNull(value,"value");
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ObjectId{" + value + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectId)) return false;
        ObjectId objectId = (ObjectId) o;
        return value.equals(objectId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}