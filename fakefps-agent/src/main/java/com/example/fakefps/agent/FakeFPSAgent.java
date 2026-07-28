package com.example.fakefps.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import org.objectweb.asm.*;

/**
 * Java Agent that intercepts Minecraft.getFps() at the bytecode level.
 * Works with any client (Lunar, Feather, Fast, vanilla, Fabric, Forge, etc.)
 *
 * ASM classes are relocated at build time to avoid classpath conflicts.
 *
 * F6 = config GUI, F7 = toggle on/off
 */
public class FakeFPSAgent {

    public static void premain(String args, Instrumentation inst) {
        FPSService.init();

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className,
                    Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                    byte[] classfileBuffer) {
                if (!"net/minecraft/client/Minecraft".equals(className)) return null;
                try {
                    ClassReader cr = new ClassReader(classfileBuffer);
                    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
                    cr.accept(new MinecraftClassVisitor(cw), 0);
                    return cw.toByteArray();
                } catch (Exception e) {
                    System.err.println("[FakeFPSAgent] Error: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }
        });

        System.out.println("[FakeFPSAgent] Loaded. " + FPSService.getDescription());
        System.out.println("[FakeFPSAgent] F7 = toggle  |  F6 = config GUI");
    }

    // -- Class visitor ---------------------------------------------------------------

    static class MinecraftClassVisitor extends ClassVisitor {

        MinecraftClassVisitor(ClassWriter cw) {
            super(Opcodes.ASM9, cw);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

            // FPS method: returns int, no args, name contains "fps"
            if ("()I".equals(desc) && isFpsCandidate(name)) {
                System.out.println("[FakeFPSAgent] Hooking method: " + name);
                return new FpsMethodVisitor(mv);
            }

            // Frame method (tick / render / run) for key polling
            String lower = name.toLowerCase();
            boolean isFrame = "tick".equals(lower) || "runtick".equals(lower)
                    || "render".equals(lower) || "run".equals(lower);
            if (isFrame && ("(Z)V".equals(desc) || "()V".equals(desc)
                    || "(F)V".equals(desc) || "(ZJ)V".equals(desc))) {
                return new TickMethodVisitor(mv);
            }

            return mv;
        }

        private boolean isFpsCandidate(String name) {
            String lower = name.toLowerCase();
            String cfg = FPSService.getConfiguredMethod();
            if (cfg != null) return cfg.equals(name);
            return "getfps".equals(lower) || "getcurrentfps".equals(lower)
                    || "fps".equals(lower) || lower.contains("fps");
        }
    }

    // -- FPS method: replaces IRETURN with INVOKESTATIC maybeReplace(I)I --------------

    static class FpsMethodVisitor extends MethodVisitor {

        FpsMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.IRETURN) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "com/example/fakefps/agent/FPSService",
                        "maybeReplace", "(I)I", false);
                super.visitInsn(Opcodes.IRETURN);
            } else {
                super.visitInsn(opcode);
            }
        }
    }

    // -- Tick method: injects checkToggle() at the start ------------------------------

    static class TickMethodVisitor extends MethodVisitor {

        TickMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/example/fakefps/agent/FPSService",
                    "checkToggle", "()V", false);
        }
    }
}
