package rengar.checker.pattern;

import java.util.HashSet;
import java.util.Set;

public class DisturbType {
    public enum Type {
        Case1, Case2, Case3, Case4, Case5, None
    }
    private final Set<Type> types = new HashSet<>();

    public DisturbType() {
        types.add(Type.None);
    }

    public boolean isDisturbFree() {
        return types.size() == 1 && types.contains(Type.None);
    }

    public void setType(Type type) {
        types.add(type);
        if (type != Type.None) {
            types.remove(Type.None);
        }
    }

    public void setType(DisturbType disturbType) {
        for (Type type : disturbType.getTypes())
            setType(type);
    }

    public Set<Type> getTypes() {
        return types;
    }

    public DisturbType copy() {
        DisturbType newType = new DisturbType();
        for (Type type : this.types) {
            newType.setType(type);
        }
        return newType;
    }
}
