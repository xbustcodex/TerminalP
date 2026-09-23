LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libtermux-bootstrap
LOCAL_SRC_FILES := termux-bootstrap-zip.S termux-bootstrap.c
include $(BUILD_SHARED_LIBRARY)

# TerminalP-owned ELF probe executed as root through $PREFIX/bin/sudo by the
# PrivilegedExecutionService bridge. The build output is packaged into the APK's
# native lib directory and installed to $PREFIX/libexec at service init by LibexecProbe.
include $(CLEAR_VARS)
LOCAL_MODULE := primetech_elf_probe
LOCAL_SRC_FILES := primetech_elf_probe.c
LOCAL_CFLAGS := -Wall -Wextra -Werror -std=c11
include $(BUILD_EXECUTABLE)

