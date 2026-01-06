export interface User {
  id: string;
  username: string;
  email: string;
  roles?: string[];
  role: string | null;
  status: string;
  tempRole?: string;
}