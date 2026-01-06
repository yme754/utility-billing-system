import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth';
import { NotificationService } from '../../../core/services/notification';

declare var window: any;

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class SidebarComponent implements OnInit {
  isAdmin = false;
  isOfficer = false; 
  isAccounts = false;
  isConsumer = false;
  username = 'User';
  workloadCount = 0; 
  private logoutModal: any;

  constructor(
    private authService: AuthService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadUserInfo();
    if (this.isOfficer) {
      this.notificationService.totalWorkloadCount$.subscribe(count => {
        this.workloadCount = count;
      });
    }
  }

  loadUserInfo() {
    const user = this.authService.getUserFromStorage();
    if (user) {
      this.username = user.username;
      const userRoles: string[] = user.roles ? user.roles : (user.role ? [user.role] : []); 
      this.isAdmin = userRoles.includes('ROLE_ADMIN');
      this.isOfficer = userRoles.includes('ROLE_BILLING_OFFICER');
      this.isAccounts = userRoles.includes('ROLE_ACCOUNTS_OFFICER');
      this.isConsumer = userRoles.includes('ROLE_CONSUMER');
      
      if (this.isOfficer) this.notificationService.refreshNotifications();
    }
  }
  getDashboardRoute(): string {
    if (this.isAdmin) return '/admin/dashboard';
    if (this.isOfficer) return '/officer/dashboard';
    if (this.isAccounts) return '/accounts/dashboard';
    return '/consumer/dashboard';
  }

  openLogoutModal() {
    const modalEl = document.getElementById('logoutModal');
    if (modalEl && window.bootstrap) {
      this.logoutModal = new window.bootstrap.Modal(modalEl);
      this.logoutModal.show();
    }
  }

  closeLogoutModal() {
    if (this.logoutModal) this.logoutModal.hide();
  }

  confirmLogout() {
    this.closeLogoutModal();
    this.authService.logout();
  }
}