import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';
import { NavbarComponent } from '../../../shared/components/navbar/navbar';
import { ConsumerService } from '../../../core/services/consumer';
import { AuthService } from '../../../core/services/auth';
import { finalize, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-consumer-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, SidebarComponent, NavbarComponent],
  templateUrl: './consumer-dashboard.component.html',
  styles: [`
    .card-custom { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); transition: transform 0.2s; }
    .card-custom:hover { transform: translateY(-3px); }
  `]
})
export class ConsumerDashboardComponent implements OnInit {
  isLoading = true;
  username = 'User';  
  consumerId = '';   
  totalDue = 0;
  activeConnections = 0;
  pendingBillsCount = 0;
  recentBills: any[] = [];
  constructor(
    private consumerService: ConsumerService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}
  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user && user.sub) {
      this.username = user.sub;
      this.initializeDashboard(user.sub);
    } else {
      this.authService.logout();
    }
  }
  initializeDashboard(userId: string) {
    this.isLoading = true;
    this.resetStats();
    this.consumerService.getProfile(userId).pipe(
      switchMap((profile: any) => {
        if (!profile) return of([]);
        this.consumerId = profile.id;         
        if (profile.firstName) this.username = profile.firstName;
        return this.consumerService.getMyConnections(this.consumerId);
      }),
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (connections) => {
        if (!connections || !Array.isArray(connections)) {
           this.activeConnections = 0;
           return;
        }
        const activeConns = connections.filter((c: any) => c.status === 'ACTIVE');
        this.activeConnections = activeConns.length;        
        activeConns.forEach(conn => {
            if (conn && conn.id) this.fetchBillsForConnection(conn.id);
        });
      },
      error: (err) => {
        console.error('Failed to load dashboard:', err);
        this.isLoading = false;
      }
    });
  }
  fetchBillsForConnection(connectionId: string) {
      this.consumerService.getBillsByConnection(connectionId).subscribe({
          next: (bills) => {
              if (!bills || bills.length === 0) return;
              const unpaid = bills.filter((b: any) => b.status === 'UNPAID');              
              unpaid.forEach((b: any) => {
                  this.totalDue += (b.amount || 0);
              });
              this.pendingBillsCount += unpaid.length;                            
              this.recentBills = [...this.recentBills, ...bills]
                .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
                .slice(0, 5);
              this.cdr.detectChanges();
          },
          error: (e) => console.error(e)
      });
  }
  private resetStats() {
    this.totalDue = 0;
    this.activeConnections = 0;
    this.pendingBillsCount = 0;
    this.recentBills = [];
  }
}