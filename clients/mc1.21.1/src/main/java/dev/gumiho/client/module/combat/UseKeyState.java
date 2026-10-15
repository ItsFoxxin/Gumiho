package dev.gumiho.client.module.combat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Physical input, never the synthetic state written by automation. */
final class UseKeyState {
    private UseKeyState() { }
    static boolean physical(Minecraft client) {
        if (!client.isWindowActive()) return false;
        var key = client.options.keyUse.getKey();
        long window = client.getWindow().getWindow();
        int value = key.getValue();
        if (value < 0) return false;
        if (key.getType() == InputConstants.Type.MOUSE)
            return value <= GLFW.GLFW_MOUSE_BUTTON_LAST && GLFW.glfwGetMouseButton(window, value) == GLFW.GLFW_PRESS;
        if (key.getType() == InputConstants.Type.KEYSYM)
            return value >= GLFW.GLFW_KEY_SPACE && value <= GLFW.GLFW_KEY_LAST
                    && GLFW.glfwGetKey(window, value) == GLFW.GLFW_PRESS;
        for (int code = GLFW.GLFW_KEY_SPACE; code <= GLFW.GLFW_KEY_LAST; code++) {
            if (GLFW.glfwGetKeyScancode(code) == value)
                return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
        }
        return false;
    }
}
