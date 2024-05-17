package appeng.container.slot;

public interface IJEITargetSlot {

    default boolean needAccept() {
        return false;
    }

}
