import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class RegisterComponent {
  user = {
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    address: '',
    phoneNumber: '',
    roles: ['consumer']
  };
  isLoading = false;  
  fieldErrors = {
    username: '',
    email: '',
    phone: ''
  };
  errorMessage = '';
  successMessage = '';

  constructor(
    private authService: AuthService, 
    private router: Router,
    private cd: ChangeDetectorRef
  ) {}

  onSubmit() {
    this.isLoading = true;
    this.resetErrors();
    this.authService.register(this.user).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.successMessage = 'Registration successful! Redirecting to login...';
        this.cd.detectChanges();
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        this.isLoading = false;
        console.log('Full Error Object:', err);
        let backendMsg = '';
        if (err.error && typeof err.error === 'object' && err.error.message) {
          backendMsg = err.error.message;
        } else if (typeof err.error === 'string') {
          backendMsg = err.error;
        } else {
          backendMsg = err.message || 'An unknown error occurred';
        }
        
        const msgLower = backendMsg.toLowerCase();
        if (msgLower.includes('username')) {
          this.fieldErrors.username = 'Username is already taken.';
        } 
        else if (msgLower.includes('email')) {
          this.fieldErrors.email = 'Email is already registered.';
        } 
        else if (msgLower.includes('phone')) {
          this.fieldErrors.phone = 'Phone number is already linked to an account.';
        } 
        else {
          this.errorMessage = backendMsg;
        }
        this.cd.detectChanges();
      }
    });
  }

  private resetErrors() {
    this.errorMessage = '';
    this.successMessage = '';
    this.fieldErrors = { username: '', email: '', phone: '' };
  }
}