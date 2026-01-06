import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';
import { NavbarComponent } from '../../../shared/components/navbar/navbar';
import { AuthService } from '../../../core/services/auth';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-defaulters',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent, NavbarComponent],
  templateUrl: './defaulters.html',
  styles: [`
    .critical-row { background-color: #fff5f5; border-left: 4px solid #dc3545; }
    .warning-row { border-left: 4px solid #ffc107; }
  `]
})
export class DefaultersComponent implements OnInit {
  defaulters: any[] = [];
  filteredList: any[] = [];
  isLoading = true;
  searchTerm = '';
  
  processingId: string | null = null;
  successMessage = '';

  constructor(
    private http: HttpClient, 
    private authService: AuthService,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadDefaulters();
  }

  loadDefaulters() {
    this.isLoading = true;
    const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
    this.http.get<any[]>('http://localhost:8080/bills/pending', { headers })
      .subscribe({
        next: (bills) => {
          this.defaulters = bills.map(bill => {
            const dueDate = new Date(bill.dueDate);
            const today = new Date();
            const diffTime = today.getTime() - dueDate.getTime();
            const daysOverdue = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
            
            return { ...bill, daysOverdue: daysOverdue };
          });
          
          this.defaulters.sort((a, b) => b.daysOverdue - a.daysOverdue);
          this.filteredList = [...this.defaulters];
          this.isLoading = false;
          this.cd.detectChanges();
        },
        error: () => {
          this.isLoading = false;
          this.cd.detectChanges();
        }
      });
  }

  sendReminder(bill: any) {
    if (this.processingId) return; 
    this.processingId = bill.id;
    this.cd.detectChanges();

    const headers = { Authorization: `Bearer ${this.authService.getToken()}` };

    this.http.post(`http://localhost:8080/bills/${bill.id}/reminder`, {}, { headers })
      .pipe(
        finalize(() => {
           this.processingId = null; 
           this.cd.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          this.showSuccess(`Reminder sent for Bill #${bill.id.substring(0,8)}`);
          bill.lastReminderSent = new Date().toISOString(); 
          this.cd.detectChanges();
        },
        error: (err) => {
          console.error(err);
          alert('Failed to send reminder.');
        }
      });
  }

  filter() {
    if (!this.searchTerm) {
      this.filteredList = [...this.defaulters];
    } else {
      const term = this.searchTerm.toLowerCase();
      this.filteredList = this.defaulters.filter(d => 
        d.id.toLowerCase().includes(term) || 
        d.connectionId.toLowerCase().includes(term)
      );
    }
    this.cd.detectChanges();
  }

  showSuccess(msg: string) {
    this.successMessage = msg;
    this.cd.detectChanges();
    setTimeout(() => {
      this.successMessage = '';
      this.cd.detectChanges();
    }, 3000);
  }
}