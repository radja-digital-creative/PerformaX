import { handleEmployeeAccount } from "../_shared/employee-account.ts";

Deno.serve(req => handleEmployeeAccount(req, "create"));
