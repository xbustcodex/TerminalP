# PrimeTech Pyunbound Audit

## Finding

No duplicate payload ownership was found between:

-   libunbound
-   libunbound-static
-   unbound
-   pyunbound

## Issue

pyunbound package contains no regular payload files.

Likely cause: Python binding installation path does not match:

lib/python\*

## Action

Inspect build logs before modifying recipes. Do not change package
metadata without reproducing the issue.
