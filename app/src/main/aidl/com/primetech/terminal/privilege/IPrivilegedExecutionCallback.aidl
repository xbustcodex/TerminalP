package com.primetech.terminal.privilege;

import com.primetech.terminal.privilege.IPrivilegedExecutionCallback;

oneway interface IPrivilegedExecutionCallback {
    void onResult(in String operationId, in String correlationId, in int statusCode,
                  in String statusMessage, in String stdout, in int exitCode);
}
