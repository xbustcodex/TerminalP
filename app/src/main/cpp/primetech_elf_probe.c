#include <unistd.h>

int main(void) {
    static const char marker[] = "PRIMETECH_ELF_OK\n";
    (void)write(STDOUT_FILENO, marker, sizeof(marker) - 1);
    return 0;
}
