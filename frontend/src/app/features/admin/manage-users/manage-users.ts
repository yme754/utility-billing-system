import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';
import { NavbarComponent } from '../../../shared/components/navbar/navbar';
import { UserManagementService } from '../../../core/services/user-management';
import { User } from '../../../models/user';

type ModalType = 'APPROVE' | 'DENY' | 'STATUS_CHANGE' | 'WARNING';

@Component({
  selector: 'app-manage-users',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent, NavbarComponent],
  templateUrl: './manage-users.html',
  styleUrls: ['./manage-users.css']
})
export class ManageUsersComponent implements OnInit {

  allUsers: User[] = [];
  filteredUsers: User[] = [];
  
  searchTerm: string = '';
  selectedStatus: string = 'ALL';
  sortBy: string = 'status';

  showModal = false;
  modalTitle = '';
  modalMessage = '';
  modalType: ModalType = 'WARNING';
  
  pendingUser: User | null = null;
  actionType: 'ACTIVATE' | 'DEACTIVATE' = 'DEACTIVATE';

  constructor(
    private userService: UserManagementService, 
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers() {
    this.userService.getAllUsers().subscribe({
      next: (data: any[]) => {
        this.allUsers = data.map(u => ({
          ...u,
          role: u.roles?.[0] ?? 'N/A',
          tempRole: '' 
        }));
        
        this.applyFilters();
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: err => {
        console.error('Failed to load users', err);
        this.allUsers = [];
        this.filteredUsers = [];
        this.cdr.detectChanges();
      }
    });
  }

  applyFilters() {
    let temp = [...this.allUsers];
    if (this.selectedStatus !== 'ALL') {
      temp = temp.filter(u => u.status === this.selectedStatus);
    }

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      temp = temp.filter(u => 
        (u.username && u.username.toLowerCase().includes(term)) ||
        (u.email && u.email.toLowerCase().includes(term))
      );
    }

    if (this.sortBy === 'username') {
      temp.sort((a, b) => a.username.localeCompare(b.username));
    } else if (this.sortBy === 'role') {
      temp.sort((a, b) => (a.role || '').localeCompare(b.role || ''));
    } else if (this.sortBy === 'status') {
      const statusPriority: { [key: string]: number } = {
        'PENDING': 1,
        'ACTIVE': 2,
        'DENIED': 3,
        'INACTIVE': 4
      };
      temp.sort((a, b) => {
        const orderA = statusPriority[a.status || ''] || 99;
        const orderB = statusPriority[b.status || ''] || 99;
        return orderA - orderB;
      });
    }

    this.filteredUsers = temp;
  }

  filterByStatus(status: string) {
    this.selectedStatus = status;
    this.applyFilters();
  }

  openModal(title: string, message: string, type: ModalType) {
    this.modalTitle = title;
    this.modalMessage = message;
    this.modalType = type;
    this.showModal = true;     
    this.cdr.detectChanges();
  }

  onApprove(user: User) {
    if (!user.tempRole) {
      this.openModal('Missing Role', 'Please assign a role before approving.', 'WARNING');
      return;
    }
    this.pendingUser = user;
    this.openModal('Confirm Approval', `Approve ${user.username} as ${user.tempRole.replace('ROLE_', '')}?`, 'APPROVE');
  }

  onDeny(user: User) {
    this.pendingUser = user;
    this.openModal('Deny User', `Are you sure you want to deny ${user.username}?`, 'DENY');
  }

  initiateStatusChange(user: User, type: 'ACTIVATE' | 'DEACTIVATE') {
    this.pendingUser = user;
    this.actionType = type;
    this.openModal(
      `${type === 'ACTIVATE' ? 'Activate' : 'Deactivate'} User`,
      `Are you sure you want to ${type.toLowerCase()} ${user.username}?`,
      'STATUS_CHANGE'
    );
  }

  handleModalConfirm() {
    if (this.modalType === 'WARNING') {
      this.closeModal();
      return;
    }
    if (!this.pendingUser) return;

    if (this.modalType === 'APPROVE') {
      this.userService.approveUser(this.pendingUser.id, this.pendingUser.tempRole!).subscribe({
        next: () => this.refreshSingleUser(this.pendingUser!.id, 'ACTIVE', this.pendingUser!.tempRole),
        error: () => this.closeModal()
      });
    }     
    else if (this.modalType === 'DENY') {
      this.userService.updateUserStatus(this.pendingUser.id, 'DENIED').subscribe({
        next: () => this.refreshSingleUser(this.pendingUser!.id, 'DENIED'),
        error: () => this.closeModal()
      });
    }     
    else if (this.modalType === 'STATUS_CHANGE') {
      const newStatus = this.actionType === 'ACTIVATE' ? 'ACTIVE' : 'INACTIVE';
      this.userService.updateUserStatus(this.pendingUser.id, newStatus).subscribe({
        next: () => this.refreshSingleUser(this.pendingUser!.id, newStatus),
        error: () => this.closeModal()
      });
    }
  }

  refreshSingleUser(id: string, status: string, role?: string) {
    const user = this.allUsers.find(u => u.id === id);
    if (user) {
      user.status = status;
      if (role) user.role = role;
      user.tempRole = '';
    }
    this.applyFilters();
    this.closeModal();
  }

  closeModal() {
    this.showModal = false;
    this.pendingUser = null;
    this.cdr.detectChanges();
  }
}