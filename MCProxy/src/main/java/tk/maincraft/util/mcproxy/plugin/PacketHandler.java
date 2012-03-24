package tk.maincraft.util.mcproxy.plugin;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import tk.maincraft.util.mcproxy.NetworkPartner;

/**
 * Methods with this interface are called when packets arrive.
 * For more information on how plugins work please take a look
 * at the <a href="https://github.com/Maincraft/MCProxy/wiki">wiki</a>.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PacketHandler {
    /**
     * @return This PacketHandler's priority. The lower it is the earlier it will be called.
     */
    short priority() default 0;

    /**
     * @return This PacketHandler will only receive packets that
     * are meant for the {@link NetworkPartner}s specified here.
     */
    NetworkPartner[] target() default { NetworkPartner.SERVER, NetworkPartner.CLIENT };
}
