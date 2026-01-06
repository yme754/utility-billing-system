import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';
import { NavbarComponent } from '../../../shared/components/navbar/navbar';
import { ConsumerService } from '../../../core/services/consumer';
import { AuthService } from '../../../core/services/auth';
import { switchMap, finalize } from 'rxjs/operators';
import { Router } from '@angular/router';
import { of } from 'rxjs';

declare var bootstrap: any;

interface TariffPlan {
  name: string;
  description: string;
  rateLabel: string;
}

@Component({
  selector: 'app-my-connections',
  standalone: true,
  imports: [CommonModule, SidebarComponent, NavbarComponent, FormsModule],
  templateUrl: './my-connections.html',
  styleUrl: './my-connections.css'
})
export class MyConnectionsComponent implements OnInit {
  isLoading = true;
  isSubmitting = false;
  showSuccessModal = false;

  connections: any[] = [];
  filteredConnections: any[] = [];
  selectedStatus = 'ALL';
  consumerId = '';

  newConnection = {
    utilityType: '',
    tariffCategory: ''
  };

  availablePlans: TariffPlan[] = [];
  readonly PLANS_DATA: { [key: string]: TariffPlan[] } = {
    'ELECTRICITY': [
      { name: 'Residential', description: 'For homes and individual households', rateLabel: '₹5.50 / kWh' },
      { name: 'Commercial', description: 'For shops, offices, and businesses', rateLabel: '₹9.20 / kWh' },
      { name: 'Industrial', description: 'High-voltage supply for factories', rateLabel: '₹11.00 / kWh' }
    ],
    'WATER': [
      { name: 'Residential', description: 'Domestic water supply connection', rateLabel: '₹15 / kL' },
      { name: 'Commercial', description: 'Commercial complex supply', rateLabel: '₹45 / kL' },
      { name: 'Industrial', description: 'Bulk supply for manufacturing', rateLabel: '₹60 / kL' }
    ],
    'GAS': [
      { name: 'Residential', description: 'Piped Natural Gas (PNG) for homes', rateLabel: '₹48 / SCM' },
      { name: 'Commercial', description: 'For restaurants and hotels', rateLabel: '₹65 / SCM' },
      { name: 'Industrial', description: 'Bulk gas for industrial heating', rateLabel: '₹58 / SCM' }
    ],
    'INTERNET': [
      { name: 'ACT Broadband 100 Mbps', description: 'High-speed residential fiber', rateLabel: '₹599 / mo' },
      { name: 'Airtel Broadband 40 Mbps', description: 'Basic plan with OTT benefits', rateLabel: '₹499 / mo' },
      { name: 'BSNL Broadband Plan', description: 'Unlimited data standard plan', rateLabel: '₹574 / mo' },
      { name: 'Commercial Broadband 300 Mbps', description: 'Business grade static IP', rateLabel: '₹2500 / mo' },
      { name: 'Hathway Broadband 100 Mbps', description: 'Value pack with router', rateLabel: '₹549 / mo' },
      { name: 'Industrial Internet 100 Mbps', description: 'SLA-backed dedicated line', rateLabel: '₹17500 / mo' },
      { name: 'Jio Fiber 100 Mbps', description: 'Unlimited data + Set Top Box', rateLabel: '₹699 / mo' }
    ]
  };

  constructor(
    private consumerService: ConsumerService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnInit(): void {
    const user = this.authService.getUserFromStorage();
    if (user && user.userId) {
      this.loadProfileAndConnections(user.userId);
    }
  }

  loadProfileAndConnections(authUserId: string) {
  this.consumerService.getProfile(authUserId).pipe(
    switchMap((profile: any) => {
      if (profile.firstName === 'New' && profile.email === 'pending@utilix.com') {
        console.log("Skeleton profile detected. Syncing registration data...");        
        const user = this.authService.getUserFromStorage(); 
        const updatedProfile = {
          ...profile,
          firstName: user.firstName || profile.firstName,
          lastName: user.lastName || profile.lastName,
          phoneNumber: user.phoneNumber || profile.phoneNumber,
          address: user.address || profile.address,
          email: user.email || profile.email,
          active: true
        };
        return this.consumerService.updateProfile(updatedProfile);
      }
      return of(profile);
    }),
    switchMap((profile: any) => {
      this.consumerId = profile.id;
      return this.consumerService.getMyConnections(this.consumerId);
    }),
    finalize(() => {
      this.isLoading = false;
      this.cdr.detectChanges();
    })
  ).subscribe({
    next: (data: any) => {
      this.connections = Array.isArray(data) ? data : [];
      this.applyFilters();
    },
    error: (err) => console.error("Profile Sync Error:", err)
  });
}

  setStatusFilter(status: string) {
    this.selectedStatus = status;
    this.applyFilters();
  }

  applyFilters() {
    this.filteredConnections = this.connections.filter(conn =>
      this.selectedStatus === 'ALL' || conn.status === this.selectedStatus
    );
    this.cdr.detectChanges();
  }

  refreshConnections() {
    if (!this.consumerId) return;
    this.consumerService.getMyConnections(this.consumerId).subscribe({
      next: (data: any) => {
        this.connections = Array.isArray(data) ? data : [];
        this.applyFilters();
      }
    });
  }
  onUtilityChange() {
    this.newConnection.tariffCategory = '';
    const type = this.newConnection.utilityType;
    
    if (type && this.PLANS_DATA[type]) {
      this.availablePlans = this.PLANS_DATA[type];
    } else {
      this.availablePlans = [];
    }
  }
  selectPlan(planName: string) {
    this.newConnection.tariffCategory = planName;
  }

  onRequestSubmit() {
    if (!this.consumerId) return;

    this.isSubmitting = true;
    const payload = {
      consumerId: this.consumerId,
      utilityType: this.newConnection.utilityType,
      tariffCategory: this.newConnection.tariffCategory
    };

    this.consumerService.requestConnection(payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        const modalEl = document.getElementById('newConnectionModal');
        if (modalEl) {
          const modalInstance = bootstrap.Modal.getInstance(modalEl);
          if (modalInstance) modalInstance.hide();
        }

        this.showSuccessModal = true;
        this.refreshConnections();

        setTimeout(() => {
          this.showSuccessModal = false;
          this.cdr.detectChanges();
        }, 3000);
      },
      error: () => {
        this.isSubmitting = false;
        alert('Failed to submit application');
      }
    });
  }
}