import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent {
  username = '';
  password = '';
  errorMessage = '';
  isLoading = false;

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit() {
    if (!this.username || !this.password) {
      this.errorMessage = 'Please enter both username and password.';
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';

    const credentials = {
      username: this.username,
      password: this.password
    };

    this.authService.login(credentials).subscribe({
      next: (res: any) => {
        this.isLoading = false;
        localStorage.setItem('userStatus', res.status);
        if (res.role === 'ROLE_ADMIN') {
          this.router.navigate(['/admin/dashboard']);
        } 
        else if (res.role === 'ROLE_BILLING_OFFICER') {
          this.router.navigate(['/officer/approvals']);
        }
        else if (res.role === 'ROLE_ACCOUNTS_OFFICER') {
          this.router.navigate(['/accounts/dashboard']);
        }
        else if (res.role === 'ROLE_CONSUMER') {
          if (res.status === 'PENDING') {
            this.router.navigate(['/pending-approval']);
          } else {
            this.router.navigate(['/consumer/dashboard']);
          }
        }
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Invalid username or password';
      }
    });
  }
}