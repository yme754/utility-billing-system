import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private tokenKey = 'utility_token';
  private userKey = 'utility_user';

  private currentUserSubject = new BehaviorSubject<any>(this.getUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();
  constructor(private http: HttpClient, private router: Router) {}
  login(credentials: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
      tap((response: any) => {
        if (response.accessToken) {
            const username = response.username || credentials.username;
            let roles = [];
            if (response.role) {
                roles = [response.role];
            } else if (response.roles) {
                roles = response.roles;
            } else {
                roles = ['consumer'];
            }
            const userId = response.userId; 
            
            this.setSession(response.accessToken, username, roles, userId);
        }
      })
    );
  }
  register(userData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
  }
  private setSession(token: string, username: string, roles: string[], userId: string) {
    localStorage.setItem(this.tokenKey, token);
    const user = { 
      userId: userId,
      username: username, 
      roles: roles
    };
    localStorage.setItem(this.userKey, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }
  logout() {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }
  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }
  getUserFromStorage(): any {
    const user = localStorage.getItem(this.userKey);
    return user ? JSON.parse(user) : null;
  }
  isLoggedIn(): boolean {
      return !!this.getToken();
  }
  getCurrentUser() {
  const token = this.getToken();
  if (!token) return null;
  return JSON.parse(atob(token.split('.')[1]));
}

changePassword(passwords: any): Observable<any> {
  const token = this.getToken();
  const headers = { 'Authorization': `Bearer ${token}` };  
  return this.http.post(`${environment.apiUrl}/auth/change-password`, passwords, { headers });
}
}
