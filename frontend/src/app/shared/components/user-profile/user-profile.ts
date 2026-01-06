import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth';
import { ConsumerService } from '../../../core/services/consumer';
import { SidebarComponent } from '../sidebar/sidebar';
import { NavbarComponent } from '../navbar/navbar';

declare var window: any;

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SidebarComponent, NavbarComponent],
  templateUrl: './user-profile.html',
  styles: [`
    .profile-header { background: linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%); color: white; }
    .avatar-wrapper { position: relative; overflow: hidden; }
    .avatar-wrapper img { transition: transform 0.3s ease; }
    .avatar-wrapper:hover img { transform: scale(1.05); }
    .form-control:disabled { background-color: #f8f9fa; opacity: 1; }
  `]
})
export class UserProfileComponent implements OnInit {
  profileForm: FormGroup;
  passwordForm: FormGroup;
  isEditing = false;
  isLoading = false;
  isChangingPassword = false;
  user: any = null;
  message = '';
  isError = false;
  
  imagePreview: string | ArrayBuffer | null = null;
  private profileExists = false; 
  private deleteModal: any;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private consumerService: ConsumerService,
    private cd: ChangeDetectorRef
  ) {
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: [{ value: '', disabled: true }], 
      phoneNumber: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      address: [''],
      role: [{ value: '', disabled: true }]
    });

    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validator: this.passwordMatchValidator });
  }

  ngOnInit() {
    this.loadProfile();
  }

  passwordMatchValidator(g: FormGroup) {
    return g.get('newPassword')?.value === g.get('confirmPassword')?.value
       ? null : {'mismatch': true};
  }

  loadProfile() {
    this.isLoading = true;
    const authUser = this.authService.getUserFromStorage();
    
    if (authUser && authUser.userId) {
      this.consumerService.getProfile(authUser.userId).subscribe({
        next: (data: any) => {
          this.user = data;
          this.profileExists = true; 
          this.populateForm(data, authUser.roles);
          this.imagePreview = data.profileImageUrl || null;
          this.isLoading = false;
          this.cd.detectChanges();
        },
        error: (err: any) => {
          if (err.status === 404) this.profileExists = false;
          this.populateForm({ 
            userId: authUser.userId,
            email: authUser.username,
            firstName: authUser.username,
            lastName: ''
          }, authUser.roles);
          this.isLoading = false;
          this.cd.detectChanges();
        }
      });
    }
  }

  populateForm(data: any, roles: any[]) {
    this.profileForm.patchValue({
      firstName: data.firstName || '',
      lastName: data.lastName || '',
      email: data.email || '',
      phoneNumber: data.phoneNumber || '',
      address: data.address || '',
      role: roles ? roles[0].replace('ROLE_', '') : 'USER'
    });
  }

  triggerFileInput() {
    document.getElementById('fileInput')?.click();
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      if (file.size > 512000) {
        this.message = 'Image too large. Please select a file under 500KB.';
        this.isError = true;
        this.cd.detectChanges();
        return;
      }
      const reader = new FileReader();
      reader.onload = () => {
        this.imagePreview = reader.result;
        this.cd.detectChanges();
        this.autoSaveProfileImage(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  }

  removePhoto() {
    const modalEl = document.getElementById('deletePhotoModal');
    if (modalEl && window.bootstrap) {
      this.deleteModal = new window.bootstrap.Modal(modalEl);
      this.deleteModal.show();
    }
  }

  closeDeleteModal() {
    if (this.deleteModal) {
      this.deleteModal.hide();
    }
  }

  confirmDeletePhoto() {
    this.closeDeleteModal();
    this.imagePreview = null;
    this.cd.detectChanges(); 
    this.autoSaveProfileImage("");
  }

  private autoSaveProfileImage(base64Image: string) {
    this.isLoading = true;
    this.cd.detectChanges();
    const authUser = this.authService.getUserFromStorage();

    const payload = {
      ...this.user,
      userId: authUser?.userId,
      profileImageUrl: base64Image
    };

    const request$ = this.profileExists 
      ? this.consumerService.updateProfile(payload) 
      : this.consumerService.createProfile(payload);

    request$.subscribe({
      next: (res: any) => {
        this.user = res;
        this.profileExists = true;
        this.isLoading = false;
        this.imagePreview = res.profileImageUrl || null;
        this.message = base64Image === "" ? 'Photo removed.' : 'Photo updated.';
        this.isError = false;
        this.cd.detectChanges();
        setTimeout(() => { this.message = ''; this.cd.detectChanges(); }, 2000);
      },
      error: (err) => {
        this.isLoading = false;
        this.isError = true;
        this.message = 'Failed to update image.';
        this.cd.detectChanges();
      }
    });
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
    if (!this.isEditing) this.loadProfile();
  }

  saveProfile() {
    if (this.profileForm.invalid) return;
    this.isLoading = true;
    this.cd.detectChanges();
    
    const formValues = this.profileForm.getRawValue();
    const authUser = this.authService.getUserFromStorage();

    const payload = {
      ...this.user,
      userId: authUser?.userId,
      firstName: formValues.firstName,
      lastName: formValues.lastName,
      email: formValues.email, 
      phoneNumber: formValues.phoneNumber,
      address: formValues.address,
      profileImageUrl: this.imagePreview 
    };

    const request$ = this.profileExists 
      ? this.consumerService.updateProfile(payload) 
      : this.consumerService.createProfile(payload);

    request$.subscribe({
      next: (res: any) => {
        this.message = 'Profile saved successfully!';
        this.isError = false;
        this.isEditing = false;
        this.isLoading = false;
        this.user = res;
        this.profileExists = true; 
        this.cd.detectChanges();
        setTimeout(() => { this.message = ''; this.cd.detectChanges(); }, 3000);
      },
      error: (err: any) => {
        this.message = 'Error saving profile.';
        this.isError = true;
        this.isLoading = false;
        this.cd.detectChanges();
      }
    });
  }

  changePassword() {
    if (this.passwordForm.invalid) return;
    this.isChangingPassword = true;
    this.cd.detectChanges();

    const pwdData = {
      currentPassword: this.passwordForm.value.currentPassword,
      newPassword: this.passwordForm.value.newPassword
    };

    this.authService.changePassword(pwdData).subscribe({
      next: () => {
        this.message = 'Password updated successfully!';
        this.isError = false;
        this.isChangingPassword = false;
        this.passwordForm.reset();
        this.cd.detectChanges();
        setTimeout(() => { this.message = ''; this.cd.detectChanges(); }, 3000);
      },
      error: (err) => {
        this.message = err.error?.message || 'Failed to update password.';
        this.isError = true;
        this.isChangingPassword = false;
        this.cd.detectChanges();
      }
    });
  }
}