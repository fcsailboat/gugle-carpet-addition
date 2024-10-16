package dev.dubhe.gugle.carpet.tools;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import com.google.gson.JsonObject;
import dev.dubhe.gugle.carpet.mixin.APAccessor;
import net.minecraft.server.level.ServerPlayer;

public class FakePlayerSerializer {

   public static JsonObject actionPackToJson(EntityPlayerActionPack actionPack) {
        JsonObject object = new JsonObject();
        EntityPlayerActionPack.Action attack = ((APAccessor) actionPack).getActions().get(EntityPlayerActionPack.ActionType.ATTACK);
        EntityPlayerActionPack.Action use = ((APAccessor) actionPack).getActions().get(EntityPlayerActionPack.ActionType.USE);
        EntityPlayerActionPack.Action jump = ((APAccessor) actionPack).getActions().get(EntityPlayerActionPack.ActionType.JUMP);
        if (attack != null && !attack.done) {
            object.addProperty("attack", attack.interval);
        }
        if (use != null && !use.done) {
            object.addProperty("use", use.interval);
        }
        if (jump != null && !jump.done) {
            object.addProperty("jump", jump.interval);
        }
        object.addProperty("sneaking", ((APAccessor) actionPack).getSneaking());
        object.addProperty("sprinting", ((APAccessor) actionPack).getSprinting());
        object.addProperty("forward", ((APAccessor) actionPack).getForward());
        object.addProperty("strafing", ((APAccessor) actionPack).getStrafing());
        return object;
    }

    public static void applyActionPackFromJson(JsonObject actions, ServerPlayer player) {
        EntityPlayerActionPack ap = ((ServerPlayerInterface) player).getActionPack();
        if (actions.has("sneaking")) ap.setSneaking(actions.get("sneaking").getAsBoolean());
        if (actions.has("sprinting")) ap.setSprinting(actions.get("sprinting").getAsBoolean());
        if (actions.has("forward")) ap.setForward(actions.get("forward").getAsFloat());
        if (actions.has("strafing")) ap.setStrafing(actions.get("strafing").getAsFloat());
        if (actions.has("attack"))
            ap.start(EntityPlayerActionPack.ActionType.ATTACK, EntityPlayerActionPack.Action.interval(actions.get("attack").getAsInt()));
        if (actions.has("use"))
            ap.start(EntityPlayerActionPack.ActionType.USE, EntityPlayerActionPack.Action.interval(actions.get("use").getAsInt()));
        if (actions.has("jump"))
            ap.start(EntityPlayerActionPack.ActionType.JUMP, EntityPlayerActionPack.Action.interval(actions.get("jump").getAsInt()));
    }
}
