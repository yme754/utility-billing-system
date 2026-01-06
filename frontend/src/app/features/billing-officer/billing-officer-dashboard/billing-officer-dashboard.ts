import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';
import { NavbarComponent } from '../../../shared/components/navbar/navbar';
import { BillingConsoleComponent } from '../../admin/billing-console/billing-console';
import { ConnectionApprovalsComponent } from '../../admin/connection-approvals/connection-approvals';

@Component({
  selector: 'app-billing-officer-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    SidebarComponent,
    NavbarComponent,
    BillingConsoleComponent,
    ConnectionApprovalsComponent
  ],
  templateUrl: './billing-officer-dashboard.html',
  styleUrls: ['./billing-officer-dashboard.css']
})
export class BillingOfficerDashboardComponent {
  activeTab: 'approvals' | 'billing' = 'approvals';
}