import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home';
import { LoginComponent } from './features/auth/login/login';
import { RegisterComponent } from './features/auth/register/register';
import { ConsumerDashboardComponent } from './features/consumer/consumer-dashboard/consumer-dashboard';
import { AuthGuard } from './core/guards/auth-guard';
import { MyConnectionsComponent } from './features/consumer/my-connections/my-connections';
import { ConnectionSuccessComponent } from './features/consumer/connection-success/connection-success';
import { ConnectionApprovalsComponent } from './features/admin/connection-approvals/connection-approvals';
import { AdminDashboardComponent } from './features/admin/admin-dashboard/admin-dashboard';
import { BillingConsoleComponent } from './features/admin/billing-console/billing-console';
import { BillHistoryComponent } from './features/consumer/bill-history/bill-history';
import { ManageUsersComponent } from './features/admin/manage-users/manage-users';
import { TariffManagementComponent } from './features/admin/tariff-management/tariff-management';
import { PendingApprovalComponent } from './features/auth/pending-approval/pending-approval';
import { AdminConnectionsComponent } from './features/admin/admin-connections/admin-connections';
import { BillingOfficerDashboardComponent } from './features/billing-officer/billing-officer-dashboard/billing-officer-dashboard';
import { AccountsDashboardComponent } from './features/accounts-officer/accounts-dashboard/accounts-dashboard';
import { TransactionLedgerComponent } from './features/accounts-officer/transaction-ledger/transaction-ledger';
import { DefaultersComponent } from './features/accounts-officer/defaulters/defaulters';
import { OfficerAllConnectionsComponent } from './features/billing-officer/all-connections/all-connections';
import { UserProfileComponent } from './shared/components/user-profile/user-profile';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'pending-approval', component: PendingApprovalComponent, canActivate: [AuthGuard]},
  { path: 'consumer/dashboard', component: ConsumerDashboardComponent, canActivate: [AuthGuard] },
  { path: 'consumer/connections', component: MyConnectionsComponent, canActivate: [AuthGuard] },
  { path: 'consumer/connection-success', component: ConnectionSuccessComponent, canActivate: [AuthGuard] },
  { path: 'consumer/bills', component: BillHistoryComponent, canActivate: [AuthGuard] },
  { path: 'consumer/bill-history', redirectTo: 'consumer/bills', pathMatch: 'full' },
  { path: 'admin/dashboard', component: AdminDashboardComponent, canActivate: [AuthGuard] },
  { path: 'admin/manage-users', component: ManageUsersComponent, canActivate: [AuthGuard] },
  { path: 'admin/approvals', component: ConnectionApprovalsComponent, canActivate: [AuthGuard] },
  { path: 'admin/connections', component: AdminConnectionsComponent, canActivate: [AuthGuard] },
  { path: 'admin/tariffs', component: TariffManagementComponent, canActivate: [AuthGuard] },
  { 
    path: 'admin/billing-console', 
    loadComponent: () => import('./features/admin/billing-console/billing-console')
      .then(m => m.BillingConsoleComponent),
    canActivate: [AuthGuard]
  },
  { path: 'officer/dashboard', component: BillingOfficerDashboardComponent, canActivate: [AuthGuard] },
  { path: 'officer/approvals', component: ConnectionApprovalsComponent, canActivate: [AuthGuard] },
  { path: 'officer/billing-console', component: BillingConsoleComponent, canActivate: [AuthGuard] },
  { path: 'accounts/dashboard', component: AccountsDashboardComponent, canActivate: [AuthGuard] },
  { path: 'accounts/transactions', component: TransactionLedgerComponent, canActivate: [AuthGuard] },
  { path: 'accounts/defaulters', component: DefaultersComponent, canActivate: [AuthGuard] },
  { 
    path: 'officer/all-connections', 
    component: OfficerAllConnectionsComponent, 
    canActivate: [AuthGuard] 
  },
  { 
    path: 'profile', 
    component: UserProfileComponent, 
    canActivate: [AuthGuard] 
  }
];