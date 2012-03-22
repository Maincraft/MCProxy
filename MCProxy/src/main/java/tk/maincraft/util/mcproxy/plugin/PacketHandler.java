package tk.maincraft.util.mcproxy.plugin;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import tk.maincraft.util.mcproxy.NetworkPartner;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PacketHandler {
    short priority() default 0;
    NetworkPartner[] target() default { NetworkPartner.SERVER, NetworkPartner.CLIENT };
}
