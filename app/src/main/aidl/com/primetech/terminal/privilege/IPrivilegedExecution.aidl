package com.primetech.terminal.privilege;

import com.primetech.terminal.privilege.IPrivilegedExecutionCallback;

interface IPrivilegedExecution {
    boolean isOperationSupported(in String operationId);
    void execute(in String operationId, in String correlationId,
                 IPrivilegedExecutionCallback callback);
}
